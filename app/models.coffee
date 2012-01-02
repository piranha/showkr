_ = require 'underscore'
Backbone = require 'backbone'
{Model, formatDate} = require 'util'
API = require 'api'


class Comment extends Model
    @field '_content'
    @field 'author'
    @field 'authorname'
    @field 'datecreate'
    @field 'iconfarm'
    @field 'iconserver'
    @field 'permalink'

    content: -> @_content()
    date: -> @datecreate()

    authorlink: ->
        "http://flickr.com/photos/#{@author()}/"

    icon: ->
        if @iconserver() == '0'
            return "http://www.flickr.com/images/buddyicon.jpg"
        "http://farm#{@iconfarm()}.static.flickr.com/#{@iconserver()}" +
            "/buddyicons/#{@author()}.jpg"


class Comments extends Backbone.Collection
    model: Comment

    initialize: (models, {@photo}) ->

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        id = @photo.id
        API.commentList @photo.id, (data) ->
            (if data.stat == 'ok' then success else error)(data)

    parse: (response) ->
        return response.comments.comment


class Photo extends Model
    # @field 'id'
    @field 'secret'
    @field 'farm'
    @field 'server'
    @field 'title'
    @field 'comments'

    initialize: ->
        if not @id
            console.log 'id undefined', @cid, @
            return
        @comments(new Comments(null, {photo: this}))

    # s  small square 75x75
    # t  thumbnail, 100 on longest side
    # m  small, 240 on longest side
    # -  medium, 500 on longest side
    # z  medium 640, 640 on longest side
    # b  large, 1024 on longest side*
    # o  original image, either a jpg, gif or png, depending on source format
    url: (size='z') ->
        "http://farm#{@farm()}.staticflickr.com/#{@server()}/" +
            "#{@id}_#{@secret()}_#{size}.jpg"

    flickrUrl: ->
        "http://www.flickr.com/photos/#{@owner()}/#{@id}/in/set-#{@setId()}/"

    owner: ->
        @collection.set.owner()

    setId: ->
        @collection.set.id


class PhotoList extends Backbone.Collection
    model: Photo

    initialize: (models, {@set}) ->

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        API.photoList @set.id, (data) ->
            (if data.stat == 'ok' then success else error)(data)

    parse: ({photoset}) ->
        return photoset.photo


class Set extends Model
    # @field 'id'
    @field 'owner'
    @field 'ownername'
    @field 'title'
    @field 'description'

    @field 'date_create'
    @field 'photos'

    # primary photo
    @field 'primary'
    @field 'secret'
    @field 'farm'
    @field 'server'

    # `photos` is taken by amount of photos
    @field 'photolist'

    @getOrCreate: (id) ->
        set = @_cache?[id]
        return set if set
        @_cache or= {}
        @_cache[id] = new Set({id: id})

    initialize: ->
        @photolist(new PhotoList(null, {set: this}))

    sync: (method, coll, {success, error}) ->
        photos = @photolist()
        API.setInfo @id, (data) ->
            (if data.stat == 'ok' then success else error)(data)
            photos.fetch()

    parse: ({photoset}) ->
        return photoset

    date: ->
        d = new Date(parseInt(@date_create()) * 1000)
        formatDate(d)


class SetList extends Backbone.Collection
    model: Set

    initialize: (models, {@user}) ->

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'
        API.setList @user.id, (data) ->
            (if data.stat == 'ok' then success else error)(data)

    parse: ({photosets}) ->
        return photosets.photoset


class User extends Model
    # that's not really a username, but who cares
    @field 'username'
    @field 'sets'

    initialize: ->
        @sets(new SetList(null, {user: this}))

    sync: (method, model, {success, error}) ->
        sets = @sets()
        if @id
            sets.fetch()
        else
            url = 'http://flickr.com/photos/' + @username()
            API.userByUrl url, (data) =>
                if data.stat != 'ok'
                    return error(data)
                @set(id: data.user.id)
                sets.fetch()


provide 'models', {Set, User, Photo}
