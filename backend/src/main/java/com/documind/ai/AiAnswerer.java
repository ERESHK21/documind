package com.documind.ai;

import java.util.List;
import java.util.Optional;

/**
 * Produces a grounded answer from retrieved context blocks.
 * Returning {@link Optional#empty()} means "no AI available" - the caller
 * then falls back to the offline extractive answerer.
 */
public interface AiAnswerer {

    Optional<String> answer(String question, List<String> contextBlocks);
}
