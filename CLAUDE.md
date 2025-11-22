The app module is the main distribution module. migrations should sit there.
Always use Instant for dates instead of LocalDate time.
put all readme files in a folder called docs
If you run the application for testing purposes. kill it afterwards
Prefer idiomatic functional kotlin like map, ?.let, {}.takeIf instead of if statements
when trying to run the application. only kill the running java application. not background processes. also don't kill docker
No * imports