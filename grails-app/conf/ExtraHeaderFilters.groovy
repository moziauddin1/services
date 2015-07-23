/**
 * Try and make IE use the latest renderer regardless of it's settings.
 */
class ExtraHeaderFilters {

    def filters = {
        all(uri: '/**') {
            after = { Map model ->
                response.setHeader('X-UA-Compatible', 'IE=Edge')
            }
        }
    }
}
