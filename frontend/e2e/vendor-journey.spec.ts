import { test, expect } from '@playwright/test';

test('should fail initially', async ({ page }) => {
  expect(true).toBe(false);
});
