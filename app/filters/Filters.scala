package filters

import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class Filters @Inject()(loggingFilter: LoggingFilter) extends DefaultHttpFilters(loggingFilter)