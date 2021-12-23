package me.philcali.device.pool.service.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface UniqueEntity {
    @JsonIgnore
    CompositeKey key();
}
