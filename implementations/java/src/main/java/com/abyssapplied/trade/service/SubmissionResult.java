package com.abyssapplied.trade.service;

import com.abyssapplied.trade.domain.DeclarationResult;

public record SubmissionResult(DeclarationResult declaration, boolean newlySubmitted) {}
