import { expect, test } from "@playwright/test";

test.describe("Vendor Journey E2E", () => {
	test("should register a vendor and onboard", async ({ page }) => {
		await page.goto("/register");

		const email = `e2e-${Date.now()}@vantage.com`;
		const password = "SecurePass123";
		const storeName = "E2E Store";
		const storeSlug = `e2e-store-${Date.now()}`;

		await page.fill('input[placeholder="Email"]', email);
		await page.fill('input[placeholder="Password (min 8 chars)"]', password);
		await page.fill('input[placeholder="Store Name"]', storeName);
		await page.fill('input[placeholder="Store Slug (e.g., my-store)"]', storeSlug);

		await page.click('button:has-text("Register")');

		await expect(page).toHaveURL("/dashboard");
	});

	test("should create a product via command palette", async ({ page }) => {
		// Assume we are logged in and on dashboard
		await page.goto("/");

		// Open command palette
		await page.keyboard.press("Meta+k");

		// Select "Add New Product"
		await page.click("text=Add New Product");

		// Fill product modal
		await page.fill('input[placeholder="Name"]', "E2E Test Mug");
		await page.fill('input[placeholder="Price"]', "15.00");
		await page.fill("textarea", "Test description");

		await page.click('button:has-text("Add Product")');

		// Verify product appears in inventory
		await page.goto("/inventory");
		await expect(page.locator('td:has-text("E2E Test Mug")')).toBeVisible();
	});

	test("should update inventory optimistically and persist", async ({ page }) => {
		await page.goto("/inventory");

		// Find the row for E2E Test Mug (we need to know its product ID, but we can use text)
		const row = page.locator('tr:has-text("E2E Test Mug")');
		await row.locator('button:has-text("Edit Quantity")').click();

		// Enter new quantity
		await row.locator('input[type="number"]').fill("50");

		await row.locator('button:has-text("Save")').click();

		// Verify optimistic update
		await expect(row.locator("td:nth-child(2)")).toHaveText("50");

		// Reload and verify persistence
		await page.reload();
		const reloadedRow = page.locator('tr:has-text("E2E Test Mug")');
		await expect(reloadedRow.locator("td:nth-child(2)")).toHaveText("50");
	});

	test("should place an order and see it in orders table", async ({ page }) => {
		await page.goto("/");

		// Open command palette
		await page.keyboard.press("Meta+k");

		// Select "Add New Product" again to create a product for order
		await page.click("text=Add New Product");
		await page.fill('input[placeholder="Name"]', "Order Test Product");
		await page.fill('input[placeholder="Price"]', "25.00");
		await page.fill("textarea", "Order test");
		await page.click('button:has-text("Add Product")');

		// Now place an order via API or UI. Since UI may not have order placement, we can call the API directly.
		// For E2E, we can use the API to place an order, then verify it appears in orders page.
		// Use page.request to make API call.
		const productId = await page.evaluate(async () => {
			const res = await fetch("/api/v1/products");
			const products = await res.json();
			const product = products.find((p) => p.name === "Order Test Product");
			return product ? product.id : null;
		});

		expect(productId).not.toBeNull();

		// Place order via API
		const orderResponse = await page.request.post("/api/v1/orders", {
			data: {
				productId: productId,
				quantity: 2,
				productName: "Order Test Product",
			},
		});
		expect(orderResponse.ok()).toBeTruthy();

		// Navigate to orders page
		await page.goto("/orders");

		// Verify order appears with status CREATED
		await expect(page.locator("text=Order Test Product")).toBeVisible();
		await expect(page.locator('td:has-text("CREATED")')).toBeVisible();
	});
});
