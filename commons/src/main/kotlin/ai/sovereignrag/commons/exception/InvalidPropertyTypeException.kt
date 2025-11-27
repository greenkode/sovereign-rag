package ai.sovereignrag.commons.exception

class InvalidPropertyTypeException(propertyName: String, propertyType: Any) :
    SrServiceException("Property $propertyName is not of type $propertyType")