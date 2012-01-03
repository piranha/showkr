_ = require 'underscore'

BASE = 'http://api.flickr.com/services/rest/'
KEY = '1606ff0ad63a3b5efeaa89443fe80704'
DEFAULTS = {api_key: KEY, format: 'json'}

API = (options, callback) ->
    query = $.toQueryString(_.extend({}, DEFAULTS, options))
    $.ajax
        url: BASE + '?' + query + '&jsoncallback=?'
        type: 'jsonp'
        success: callback

# adding methods to an API client
#
# expects api client to conform to receive options mapping and a callback as
# arguments
#
# expects methods mapping as a second parameter, which should look like:
#
#     methodName: {
#         args: ['arg1', 'arg2'],
#         someProperty: 'this.property.content'
#     }
#
# where:
#
#  - `methodName` is a name of this method on API client then
#  - `args` is a list of arguments this method will receive (excluding callback,
#    which is last argument
#  - everything else is just copied as is on `options` mapping
#
# example:
#
#     addMethods(flickrApi, {
#         commentList: {
#             args: ['photo_id'],
#             method: 'flickr.photos.comments.getList'
#         }
#     })

addMethods = (apiClient, methods) ->
    _.each methods, (props, name) ->
        apiClient[name] = (args..., callback) ->
            options = {}
            for arg, i in props.args
                options[arg] = args[i]
            for key, value of props
                if key != 'args'
                    options[key] = value
            apiClient(options, callback)

addMethods API,
    photoList:
        args: ['photoset_id']
        method: 'flickr.photosets.getPhotos'
    photoComments:
        args: ['photo_id']
        method: 'flickr.photos.comments.getList'
    setInfo:
        args: ['photoset_id']
        method: 'flickr.photosets.getInfo'
    setList:
        args: ['user_id']
        method: 'flickr.photosets.getList'
    setComments:
        args: ['photoset_id']
        method: 'flickr.photosets.comments.getList'
    userByUrl:
        args: ['url']
        method: 'flickr.urls.lookupUser'

provide 'api', API
