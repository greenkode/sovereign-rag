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