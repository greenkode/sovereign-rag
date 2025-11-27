package ai.sovereignrag.commons.exception

class PropertyNotFoundException(propertyName: String) : SrServiceException("Property $propertyName not found")