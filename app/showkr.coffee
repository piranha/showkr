$ = ender
_ = require 'underscore'
Backbone = require 'backbone'
{addOrPromote, View} = require 'util'
{Set} = require 'models'
{SetView} = require 'viewing'
{UserView} = require 'browsing'


class About extends Backbone.View
    template: require 'templates/about.eco'
    render: ->
        @el.innerHTML = @template()
        this


class Form extends Backbone.View
    tagName: 'form'
    template: require 'templates/form.eco'

    events:
        'submit': 'submit'

    initialize: ({@app}) ->
        @app.bind 'history:change', @render, this

    render: ->
        @el.innerHTML = @template
            history: @app.getHistory()
        this

    submit: (e) ->
        e.preventDefault()
        {url, user} = $(e.target).serialize(type: 'map')
        @$('input[type=text]').val('')
        if url
            @processUrl(url)
        else if user
            @processUser(user)
        else
            alert 'you have not entered anything'

    processUrl: (url) ->
        if url.match(/^\d+$/)
            set = url
        else if url.match(/\/sets\/([^\/]+)/)
            set = url.match(/\/sets\/([^\/]+)/)[1]
        else
            return alert 'something is wrong in your input'

        @app.navigate(set, true)

    processUser: (user) ->
        @app.navigate("user/#{user}", true)


class @Showkr extends Backbone.Router
    routes:
        '': 'index'
        'about': 'about'
        'user/:user': 'user'
        ':set': 'set'
        ':set/:photo': 'set'

    defaults:
        set: null
        title: true

    initialize: (el='#main', config) ->
        @config = _.extend({}, @defaults, config)
        @views = {}
        @el = $(el)[0]
        $.key 'h', _.bind(@showHelp, @)
        history = Backbone.history

        init = =>
            if @config.set
                @set(@config.set)
            else if @config.user
                if not location.hash or not history.loadUrl(location.hash)
                    @navigate("user/#{@config.user}", false)
                history.start()
            else
                history.start()

        setTimeout init, 1

    # returns a view (creates if necessary) and switches to it
    getView: (id, creator) ->
        view = @views[id]
        if not view
            view = @views[id] = creator()
            isNew = true
        else
            isNew = false

        if @current and @current != view
            @current.el.style.display = 'none'
        @current = view

        if isNew
            @el.appendChild view.render().el
        else
            view.el.style.display = 'block'

        return [view, isNew]

    # ## Views

    index: ->
        [form, isNew] = @getView('form', => new Form(app: this))

    set: (set, photo) ->
        [view, isNew] = @getView("set-#{set}", =>
            new SetView(id: set, config: @config))
        if photo
            view.scrollTo(photo)

    user: (user) ->
        [view, isNew] = @getView("user-#{user}", =>
            new UserView(user: user, config: @config))

    about: ->
        @getView('about', -> new About())

    # ## Helpers

    showHelp: ->
        $('#help').overlay().open()

    addToHistory: (set) ->
        history = JSON.parse(localStorage.showkr or '[]')
        history = addOrPromote(history, [set.id, set.title()])
        history = history[..20]
        changed = localStorage.showkr != JSON.stringify(history)
        localStorage.showkr = JSON.stringify(history)
        if changed
            @trigger('history:change', this)

    getHistory: ->
        JSON.parse(localStorage.showkr or '[]')
