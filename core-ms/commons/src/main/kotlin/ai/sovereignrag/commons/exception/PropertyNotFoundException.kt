package ai.sovereignrag.commons.exception

import ai.sovereignrag.commons.exception.SrServiceException

class PropertyNotFoundException(propertyName: String) : SrServiceException("Property $propertyName not found")