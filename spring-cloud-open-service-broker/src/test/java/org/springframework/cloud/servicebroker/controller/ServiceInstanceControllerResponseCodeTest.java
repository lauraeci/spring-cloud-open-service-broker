/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.servicebroker.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceUpdateNotSupportedException;
import org.springframework.cloud.servicebroker.model.instance.AsyncServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.error.ErrorMessage;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.CatalogService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class ServiceInstanceControllerResponseCodeTest {

	private final CatalogService catalogService = mock(CatalogService.class);
	private final ServiceInstanceService serviceInstanceService = mock(ServiceInstanceService.class);

	private final Map<String, String> pathVariables = Collections.emptyMap();

	private ServiceInstanceController controller;

	@DataPoints("createResponsesWithExpectedStatus")
	public static List<CreateResponseAndExpectedStatus> createDataPoints() {
		return Arrays.asList(
				new CreateResponseAndExpectedStatus(
						null,
						HttpStatus.CREATED
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceResponse.builder()
								.async(false)
								.instanceExisted(false)
								.dashboardUrl("https://dashboard.example.com")
								.build(),
						HttpStatus.CREATED
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceResponse.builder()
								.async(false)
								.instanceExisted(true)
								.build(),
						HttpStatus.OK
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceResponse.builder()
								.async(true)
								.instanceExisted(false)
								.operation("creating")
								.build(),
						HttpStatus.ACCEPTED
				),
				new CreateResponseAndExpectedStatus(
						CreateServiceInstanceResponse.builder()
								.async(true)
								.instanceExisted(true)
								.build(),
						HttpStatus.ACCEPTED
				)
		);
	}

	@DataPoints("getResponsesWithExpectedStatus")
	public static List<GetResponseAndExpectedStatus> getDataPoints() {
		return Arrays.asList(
				new GetResponseAndExpectedStatus(
						null,
						HttpStatus.OK
				),
				new GetResponseAndExpectedStatus(
						GetServiceInstanceResponse.builder()
								.serviceDefinitionId("service-definition-id")
								.planId("plan-id")
								.build(),
						HttpStatus.OK
				)
		);
	}

	@DataPoints("updateResponsesWithExpectedStatus")
	public static List<UpdateResponseAndExpectedStatus> updateDataPoints() {
		return Arrays.asList(
				new UpdateResponseAndExpectedStatus(
						null,
						HttpStatus.OK
				),
				new UpdateResponseAndExpectedStatus(
						UpdateServiceInstanceResponse.builder()
								.async(false)
								.build(),
						HttpStatus.OK
				),
				new UpdateResponseAndExpectedStatus(
						UpdateServiceInstanceResponse.builder()
								.async(true)
								.operation("updating")
								.build(),
						HttpStatus.ACCEPTED
				)
		);
	}

	@DataPoints("deleteResponsesWithExpectedStatus")
	public static List<DeleteResponseAndExpectedStatus> deleteDataPoints() {
		return Arrays.asList(
				new DeleteResponseAndExpectedStatus(
						null,
						HttpStatus.OK
				),
				new DeleteResponseAndExpectedStatus(
						DeleteServiceInstanceResponse.builder()
								.async(false)
								.build(),
						HttpStatus.OK
				),
				new DeleteResponseAndExpectedStatus(
						DeleteServiceInstanceResponse.builder()
								.async(true)
								.operation("deleting")
								.build(),
						HttpStatus.ACCEPTED
				)
		);
	}

	@DataPoints("lastOperationResponsesWithExpectedStatus")
	public static List<GetLastOperationResponseAndExpectedStatus> lastOperationDataPoints() {
		return Arrays.asList(
				new GetLastOperationResponseAndExpectedStatus(
						GetLastServiceOperationResponse.builder()
								.operationState(OperationState.IN_PROGRESS)
								.description("in progress")
								.build(),
						HttpStatus.OK
				),
				new GetLastOperationResponseAndExpectedStatus(
						GetLastServiceOperationResponse.builder()
								.operationState(OperationState.SUCCEEDED)
								.deleteOperation(false)
								.build(),
						HttpStatus.OK
				),
				new GetLastOperationResponseAndExpectedStatus(
						GetLastServiceOperationResponse.builder()
								.operationState(OperationState.SUCCEEDED)
								.deleteOperation(true)
								.build(),
						HttpStatus.GONE
				),
				new GetLastOperationResponseAndExpectedStatus(
						GetLastServiceOperationResponse.builder()
								.operationState(OperationState.FAILED)
								.build(),
						HttpStatus.OK
				)
		);
	}

	@Before
	public void setUp() {
		controller = new ServiceInstanceController(catalogService, serviceInstanceService);

		when(catalogService.getServiceDefinition(anyString()))
				.thenReturn(ServiceDefinition.builder().build());
	}

	@Theory
	public void createServiceInstanceWithResponseGivesExpectedStatus(CreateResponseAndExpectedStatus data) {
		when(serviceInstanceService.createServiceInstance(any(CreateServiceInstanceRequest.class)))
						.thenReturn(data.response);

		CreateServiceInstanceRequest createRequest = CreateServiceInstanceRequest.builder()
				.serviceDefinitionId("service-definition-id")
				.build();

		ResponseEntity<CreateServiceInstanceResponse> responseEntity = controller
				.createServiceInstance(pathVariables, null, false, null, null,
						createRequest);

		assertThat(responseEntity.getStatusCode()).isEqualTo(data.expectedStatus);
		assertThat(responseEntity.getBody()).isEqualTo(data.response);
	}

	@Theory
	public void getServiceInstanceWithResponseGivesExpectedStatus(GetResponseAndExpectedStatus data) {
		when(serviceInstanceService.getServiceInstance(any(GetServiceInstanceRequest.class)))
						.thenReturn(data.response);

		ResponseEntity<GetServiceInstanceResponse> responseEntity = controller
				.getServiceInstance(pathVariables, null, null, null);

		assertThat(responseEntity.getStatusCode()).isEqualTo(data.expectedStatus);
		assertThat(responseEntity.getBody()).isEqualTo(data.response);
	}

	@Theory
	public void deleteServiceInstanceWithResponseGivesExpectedStatus(DeleteResponseAndExpectedStatus data) {
		when(serviceInstanceService.deleteServiceInstance(any(DeleteServiceInstanceRequest.class)))
				.thenReturn(data.response);

		ResponseEntity<DeleteServiceInstanceResponse> responseEntity = controller
				.deleteServiceInstance(pathVariables, null, "service-definition-id", null, false, null, null);

		assertThat(responseEntity.getStatusCode()).isEqualTo(data.expectedStatus);
		assertThat(responseEntity.getBody()).isEqualTo(data.response);
	}

	@Test
	public void deleteServiceInstanceWithMissingInstanceGivesExpectedStatus() {
		when(serviceInstanceService.deleteServiceInstance(any(DeleteServiceInstanceRequest.class)))
				.thenThrow(new ServiceInstanceDoesNotExistException("instance does not exist"));

		ResponseEntity<DeleteServiceInstanceResponse> responseEntity = controller
				.deleteServiceInstance(pathVariables, null, "service-definition-id", null, false, null, null);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.GONE);
		assertThat(responseEntity.getBody()).isEqualTo(DeleteServiceInstanceResponse.builder().build());
	}

	@Theory
	public void updateServiceInstanceWithResponseGivesExpectedStatus(UpdateResponseAndExpectedStatus data) {
		when(serviceInstanceService.updateServiceInstance(any(UpdateServiceInstanceRequest.class)))
				.thenReturn(data.response);

		UpdateServiceInstanceRequest updateRequest = UpdateServiceInstanceRequest.builder()
				.serviceDefinitionId("service-definition-id")
				.build();

		ResponseEntity<UpdateServiceInstanceResponse> responseEntity = controller
				.updateServiceInstance(pathVariables, null, false, null, null,
						updateRequest);

		assertThat(responseEntity.getStatusCode()).isEqualTo(data.expectedStatus);
		assertThat(responseEntity.getBody()).isEqualTo(data.response);
	}

	@Theory
	public void getLastOperationWithResponseGivesExpectedStatus(GetLastOperationResponseAndExpectedStatus data) {
		when(serviceInstanceService.getLastOperation(any(GetLastServiceOperationRequest.class)))
				.thenReturn(data.response);

		ResponseEntity<GetLastServiceOperationResponse> responseEntity = controller
				.getServiceInstanceLastOperation(pathVariables, null, null, null, null, null, null);

		assertThat(responseEntity.getStatusCode()).isEqualTo(data.expectedStatus);
		assertThat(responseEntity.getBody()).isEqualTo(data.response);
	}

	@Test
	public void serviceInstanceExistsExceptionGivesExpectedStatus() {
		ServiceInstanceExistsException exception =
				new ServiceInstanceExistsException("service-instance-id", "service-definition-id");

		ResponseEntity<ErrorMessage> responseEntity = controller.handleException(exception);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(responseEntity.getBody().getMessage()).contains("serviceInstanceId=service-instance-id");
		assertThat(responseEntity.getBody().getMessage()).contains("serviceDefinitionId=service-definition-id");
	}

	@Test
	public void serviceInstanceUpdateNotSupportedExceptionGivesExpectedStatus() {
		ServiceInstanceUpdateNotSupportedException exception = new
				ServiceInstanceUpdateNotSupportedException("test exception");

		ResponseEntity<ErrorMessage> responseEntity = controller.handleException(exception);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(responseEntity.getBody().getMessage()).contains("test exception");
	}

	public static class AsyncResponseAndExpectedStatus<T extends AsyncServiceInstanceResponse> {
		public final T response;
		public final HttpStatus expectedStatus;

		public AsyncResponseAndExpectedStatus(T response, HttpStatus expectedStatus) {
			this.response = response;
			this.expectedStatus = expectedStatus;
		}

		@Override
		public String toString() {
			String responseValue = response == null ? "null" :
					"{" +
							"async=" + response.isAsync() +
					"}";

			return "response=" + responseValue +
					", expectedStatus=" + expectedStatus;
		}
	}

	public static class CreateResponseAndExpectedStatus
			extends AsyncResponseAndExpectedStatus<CreateServiceInstanceResponse> {
		public CreateResponseAndExpectedStatus(CreateServiceInstanceResponse response, HttpStatus expectedStatus) {
			super(response, expectedStatus);
		}

		@Override
		public String toString() {
			String responseValue = response == null ? "null" :
					"{" +
							"async=" + response.isAsync() +
							", instanceExisted=" + response.isInstanceExisted() +
					"}";

			return "response=" + responseValue +
					", expectedStatus=" + expectedStatus;
		}

	}

	public static class GetResponseAndExpectedStatus {
		public final GetServiceInstanceResponse response;
		public final HttpStatus expectedStatus;

		public GetResponseAndExpectedStatus(GetServiceInstanceResponse response,
											HttpStatus expectedStatus) {
			this.response = response;
			this.expectedStatus = expectedStatus;
		}
	}

	public static class UpdateResponseAndExpectedStatus
			extends AsyncResponseAndExpectedStatus<UpdateServiceInstanceResponse> {
		public UpdateResponseAndExpectedStatus(UpdateServiceInstanceResponse response, HttpStatus expectedStatus) {
			super(response, expectedStatus);
		}
	}

	public static class DeleteResponseAndExpectedStatus
			extends AsyncResponseAndExpectedStatus<DeleteServiceInstanceResponse> {
		public DeleteResponseAndExpectedStatus(DeleteServiceInstanceResponse response, HttpStatus expectedStatus) {
			super(response, expectedStatus);
		}
	}

	public static class GetLastOperationResponseAndExpectedStatus {
		public final GetLastServiceOperationResponse response;
		public final HttpStatus expectedStatus;

		public GetLastOperationResponseAndExpectedStatus(GetLastServiceOperationResponse response,
														 HttpStatus expectedStatus) {
			this.response = response;
			this.expectedStatus = expectedStatus;
		}

		@Override
		public String toString() {
			String responseValue = response == null ? "null" :
					"{" +
							"state=" + response.getState() +
							", description=" + response.getDescription() +
							", deleteOperation=" + response.isDeleteOperation() +
					"}";

			return "response=" + responseValue +
					", expectedStatus=" + expectedStatus;
		}
	}
}
