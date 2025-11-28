The app module is the main distribution module. migrations should sit there.
Always use Instant for dates instead of LocalDate time.
put all readme files in a folder called docs
If you run the application for testing purposes. kill it afterwards
Prefer idiomatic functional kotlin like map, ?.let, {}.takeIf instead of if statements
when trying to run the application. only kill the running java application. not background processes. also don't kill docker
No * imports
In future database migrations, don't add comments
All migrations should be idempotent
don't generate documentation unless explicitly stated
all materialized views should be created as repeatable migrations
materialized views always go in the sql/views folder and functions go in the sql/functions folder
all functions are repeatable migrations
don't generate md files or documentation in the future unless explicitly instructed to
always use KotlinLogging when creating loggers
don't add any Jakarta validations in the future
don't use * imports
do not add comments
never use Any in type parameters
don't generate @Column annotations
Always use log for naming the kotlin logger rather than logger
don't use the try return pattern anymore
always use the cqrs pattern request -> command -> result -> response commandhandlers/queryhandlers from controllers
don't commit automatically
store md files in the docs folder
dont use jdbctemplate unless explicitly told to
Follow existing patterns and conventions in the codebase
Use explicit type declarations where it improves readability
Prefer composition over inheritance
Use descriptive variable and method names
prefer idiomatic and functional kotiln instead of if/else try/catch type structures
prefer things like ?.let {} or takeIf{} instead of if else
spring boot starters should be in the app or identity-app modules, 
libraries that are reused in multiple modules should be imported into the parent module e.g. the core-ms or identity-ms pom file.
don't add inter module dependencies, rather create gateway interfaces in the commons and services in the modules that implement the gateway interfaces
the gateways can be called from anywhere.
Always use internationalization with any error or information messages going back to the frontend.
Always create unit tests for command and query handlers.
create integration tests for service methods using test containers.
create end to end tests using playwright.