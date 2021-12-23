package me.philcali.device.pool.service.api;


import me.philcali.device.pool.service.api.model.CreateProvisionObject;
import me.philcali.device.pool.service.api.model.ProvisionObject;
import me.philcali.device.pool.service.api.model.UpdateProvisionObject;

public interface ProvisionRepo extends ObjectRepository<ProvisionObject, CreateProvisionObject, UpdateProvisionObject> {
    int MAX_ITEMS = 100;
}
