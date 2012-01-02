Sk = Ember.Application.extend({
    ready: function() {
        console.log('123');
    }
});

Sk.Photo = Ember.Object.extend({
    id: null,
    secret: null,
    farm: null,
    server: null,
    title: null,

    url: function () {
        var size = 'z';
        return format("http://farm%s.staticflickr.com/%s/%s_%s_%s.jpg",
                      this.get('farm'),
                      this.get('server'),
                      this.get('id'),
                      this.get('secret'),
                      size);
    }.property('farm', 'server', 'id', 'secret'),

    flickrUrl: function() {
        return format("http://www.flickr.com/photos/%s/%s/in/set-%s/",
                      this.get('owner'),
                      this.get('id'),
                      this.get('setId'));
    }.property('id')
});

Sk.PhotoView = Ember.View.extend({
    templateName: 'photo'
});

Sk.photosController = Ember.ArrayController.create({
    content: [],
    base: 'http://api.flickr.com/services/rest/',
    key: '1606ff0ad63a3b5efeaa89443fe80704',
    photoset_id: '72157625971808681',
    model: Sk.Photo,

    api: function(options) {
        var callback = options.callback;
        delete options.callback;

        options.api_key = this.key;
        options.format = 'json';
        $.ajax({
            url: this.base + '?' + $.param(options) + '&jsoncallback=?',
            dataType: 'jsonp',
            success: callback
        });
    },

    loadPhotos: function() {
        var self = this;

        this.api({
            method: 'flickr.photosets.getPhotos',
            photoset_id: this.photoset_id,
            callback: function(data) {
                var photos = data.photoset.photo.map(function(item) {
                    return self.createObject(item, data);
                });
                self.set('content', photos);
            }
        });
    },

    createObject: function(obj, data) {
        obj.setId = this.photoset_id;
        obj.owner = data.photoset.owner;
        return this.model.create(obj);
    }
});
