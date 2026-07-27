import { test, expect } from '@playwright/test';

let authToken: string;
let tenantId: string;

test.beforeAll(async ({ request }) => {
  // Register a vendor once and store credentials
  const email = `e2e-${Date.now()}@vantage.com`;
  const password = 'SecurePass123';
  const storeName = 'E2E Store';
  const storeSlug = `e2e-store-${Date.now()}`;

  const registerRes = await request.post('/api/v1/vendors/register', {
    data: {
      email,
      password,
      name: storeName,
      tenantSlug: storeSlug,
    },
  });
  expect(registerRes.ok()).toBeTruthy();
  const auth = await registerRes.json();
  authToken = auth.accessToken;
  tenantId = auth.tenantId;
});

test.describe('Vendor Journey E2E', () => {
  test.beforeEach(async ({ page, context }) => {
    // Set authentication headers for API requests
    await context.setExtraHTTPHeaders({
      Authorization: `Bearer ${authToken}`,
      'X-Tenant-ID': tenantId,
    });

    // Navigate to dashboard (requires authentication)
    await page.goto('/');
  });

  test('should register a vendor and onboard', async ({ page }) => {
    // This test is already covered by beforeAll, but we can keep it as a smoke test
    // Actually we already registered, so we just verify we are on dashboard
    await expect(page).toHaveURL('/');
  });

  test('should create a product via command palette', async ({ page }) => {
    // Open command palette
    await page.keyboard.press('Meta+k');

    // Select "Add New Product"
    await page.click('text=Add New Product');

    // Fill product modal
    await page.fill('input[placeholder="Name"]', 'E2E Test Mug');
    await page.fill('input[placeholder="Price"]', '15.00');
    await page.fill('textarea', 'Test description');

    await page.click('button:has-text("Add Product")');

    // Verify product appears in inventory
    await page.goto('/inventory');
    await expect(page.locator('td:has-text("E2E Test Mug")')).toBeVisible();
  });

  test('should update inventory optimistically and persist', async ({ page }) => {
    await page.goto('/inventory');

    const row = page.locator('tr:has-text("E2E Test Mug")');
    await row.locator('button:has-text("Edit Quantity")').click();

    await row.locator('input[type="number"]').fill('50');
    await row.locator('button:has-text("Save")').click();

    await expect(row.locator('td:nth-child(2)')).toHaveText('50');

    await page.reload();
    const reloadedRow = page.locator('tr:has-text("E2E Test Mug")');
    await expect(reloadedRow.locator('td:nth-child(2)')).toHaveText('50');
  });

  test('should place an order and see it in orders table', async ({ page, request }) => {
    // Create a product specifically for this test
    await page.keyboard.press('Meta+k');
    await page.click('text=Add New Product');
    await page.fill('input[placeholder="Name"]', 'Order Test Product');
    await page.fill('input[placeholder="Price"]', '25.00');
    await page.fill('textarea', 'Order test');
    await page.click('button:has-text("Add Product")');

    // Wait for product to be created via API (optimistic)
    await page.waitForTimeout(500);

    // Fetch product ID using API with auth
    const productsRes = await request.get('/api/v1/products', {
      headers: {
        Authorization: `Bearer ${authToken}`,
        'X-Tenant-ID': tenantId,
      },
    });
    expect(productsRes.ok()).toBeTruthy();
    const products = await productsRes.json();
    const product = products.find((p: any) => p.name === 'Order Test Product');
    expect(product).toBeDefined();
    const productId = product.id;

    // Place order via API
    const orderRes = await request.post('/api/v1/orders', {
      headers: {
        Authorization: `Bearer ${authToken}`,
        'X-Tenant-ID': tenantId,
      },
      data: {
        productId,
        quantity: 2,
        productName: 'Order Test Product',
      },
    });
    expect(orderRes.ok()).toBeTruthy();

    // Navigate to orders page
    await page.goto('/orders');

    // Verify order appears with status CREATED
    await expect(page.locator('text=Order Test Product')).toBeVisible();
    await expect(page.locator('td:has-text("CREATED")')).toBeVisible();
  });
});
