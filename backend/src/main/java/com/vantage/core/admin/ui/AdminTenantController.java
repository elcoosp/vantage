package com.vantage.core.admin.ui;

import com.vantage.core.admin.app.AdminTenantService;
import com.vantage.core.admin.ui.dto.StatusUpdateRequest;
import com.vantage.core.admin.ui.dto.TenantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tenants")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;

    public AdminTenantController(AdminTenantService adminTenantService) {
        this.adminTenantService = adminTenantService;
    }

    @GetMapping
    public List<TenantResponse> getAllTenants() {
        return adminTenantService.getAllTenants();
    }

    @PutMapping("/{tenantId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@PathVariable UUID tenantId, @RequestBody StatusUpdateRequest request) {
        adminTenantService.updateStatus(tenantId, request.status());
    }
}
