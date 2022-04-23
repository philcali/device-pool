/*
 * Copyright (c) 2022 Philip Cali
 * Released under Apache-2.0 License
 *     (https://www.apache.org/licenses/LICENSE-2.0)
 */

package me.philcali.device.pool.service.rpc.model;

import me.philcali.device.pool.service.api.model.CompositeKey;
import me.philcali.device.pool.service.api.model.ProvisionObject;

public interface AccountRequest {
    CompositeKey accountKey();

    ProvisionObject provision();
}
