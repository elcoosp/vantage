package com.vantage.core.tenant;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Propagates the resolved tenant into the GraphQL execution context.
 *
 * <p>The tenant is established on the inbound servlet thread by {@link TenantFilter} and stored in
 * the {@link TenantContext} thread-local. Spring for GraphQL executes the query on a separate task
 * thread ({@code task-N}), so the thread-local is no longer visible to {@code @QueryMapping}
 * resolvers. This interceptor runs on the request thread (where the tenant is available) and copies
 * it into the GraphQL context, which is carried across threads and exposed to resolvers via
 * {@code DataFetchingEnvironment.getGraphQLContext()}.</p>
 */
@Component
public class TenantGraphQlInterceptor implements WebGraphQlInterceptor {

    public static final String TENANT_KEY = "tenantId";

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            request.configureExecutionInput((executionInput, builder) ->
                    builder.graphQLContext(c -> c.put(TENANT_KEY, tenantId)).build());
        }
        return chain.next(request);
    }
}
