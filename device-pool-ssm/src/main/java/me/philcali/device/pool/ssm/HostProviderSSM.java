/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.ssm;

import me.philcali.device.pool.model.APIShadowModel;
import me.philcali.device.pool.provision.ExpandingHostProvider;
import org.immutables.value.Value;
import software.amazon.awssdk.services.ssm.SsmClient;

@APIShadowModel
@Value.Immutable
@Deprecated
public abstract class HostProviderSSM extends ExpandingHostProvider implements HostProvisionMixin {
    private static final int DEFAULT_CACHE_THRESHOLD = 100;

    abstract SsmClient ssm();

    @Value.Default
    @Deprecated
    int cacheThreshold() {
        return DEFAULT_CACHE_THRESHOLD;
    }

    @Override
    public int maximumIncrementalLeases() {
        return DEFAULT_CACHE_THRESHOLD;
    }

    public static final class Builder extends ImmutableHostProviderSSM.Builder {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ExpansionFunction expansionFunction() {
        return HostExpansionSSM.builder()
                .architecture(architecture())
                .poolId(poolId())
                .ssm(ssm())
                .filters(filters())
                .hostBuilder(hostBuilder())
                .build();
    }

    @Override
    public void close() {
        super.close();
        ssm().close();
    }
}
