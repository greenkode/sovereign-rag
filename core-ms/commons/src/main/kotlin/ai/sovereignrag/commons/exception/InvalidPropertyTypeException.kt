package ai.sovereignrag.commons.exception

import ai.sovereignrag.commons.exception.SrServiceException

class InvalidPropertyTypeException(propertyName: String, propertyType: Any) :
    SrServiceException("Property $propertyName is not of type $propertyType")