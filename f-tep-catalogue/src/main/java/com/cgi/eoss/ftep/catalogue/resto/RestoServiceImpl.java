package com.cgi.eoss.ftep.catalogue.resto;

import org.geojson.Feature;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * <p>Implementation of RestoService using Resto's HTTP REST-style API.</p>
 */
@Component
public class RestoServiceImpl implements RestoService {
    @Override
    public UUID ingest(Feature feature) {
        // TODO Implement
        throw new UnsupportedOperationException("Resto ingestion not yet supported");
    }
}
