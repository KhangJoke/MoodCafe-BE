package com.moodcafe.store.abstraction.service;

import com.moodcafe.store.dto.request.CreateVisitSnapRequest;
import com.moodcafe.store.dto.request.SubmitMicroSurveyRequest;
import com.moodcafe.store.dto.response.ActiveVisitStatusResponse;
import com.moodcafe.store.dto.response.MicroSurveyQuestionDto;
import com.moodcafe.store.dto.response.SurveySubmissionResponse;
import com.moodcafe.store.dto.response.VisitVerificationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface VisitVerificationService {

    VisitVerificationResponse snapVisit(UUID storeId, CreateVisitSnapRequest request, MultipartFile image);

    MicroSurveyQuestionDto getStoreMicroSurveyQuestion(UUID storeId);

    MicroSurveyQuestionDto getVisitMicroSurveyQuestion(UUID visitVerificationId);

    SurveySubmissionResponse submitMicroSurvey(UUID visitVerificationId, SubmitMicroSurveyRequest request);

    ActiveVisitStatusResponse getActiveVisitStatus(UUID storeId);

    VisitVerificationResponse getVisitVerificationById(UUID visitVerificationId);
}
