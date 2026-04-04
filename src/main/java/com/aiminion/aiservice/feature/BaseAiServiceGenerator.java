package com.aiminion.aiservice.feature;

/**
 * Generic contract for all AI feature generators.
 *
 * @param <REQ> Feature-specific request type
 * @param <RES> Feature-specific response type
 */
public interface BaseAiServiceGenerator<REQ, RES> {
    RES generate(REQ req);
}
