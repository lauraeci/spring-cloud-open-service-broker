/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.servicebroker.service;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;

/**
 * Default implementation of ServiceInstanceBindingService for service brokers that do not support bindable services.
 *
 * See http://docs.cloudfoundry.org/services/api.html#binding
 *
 * @author Scott Frederick
 */
public class NonBindableServiceInstanceBindingService implements ServiceInstanceBindingService {
	@Override
	public CreateServiceInstanceBindingResponse createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {
		throw nonBindableException();
	}

	@Override
	public void deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {
		throw nonBindableException();
	}

	private UnsupportedOperationException nonBindableException() {
		return new UnsupportedOperationException("This service broker does not support bindable services. " +
				"The service broker should set 'bindable: false' in the service catalog for all service offerings, " +
				"or provide an implementation of this API.");
	}
}
