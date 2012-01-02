_ = require 'underscore'
Backbone = require 'backbone'
{addOrPromote, View} = require 'util'
{Set} = require 'models'
{SetView} = require 'viewing'
{UserView} = require 'browsing'


class Form extends Backbone.View
    tagName: 'form'

    events:
        'submit': 'submit'

    initialize: ({@app}) ->
        @template = _.template($('#form-template').html())
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
        'user/:user': 'user'
        ':set': 'set'
        ':set/:photo': 'set'

    initialize: (el='#main') ->
        @views = {}
        @el = $(el)
        $.key 'shift+/', _.bind(@showHelp, @)
        setTimeout (-> Backbone.history.start()), 1

    # returns a view (creates if necessary) and switches to it
    getView: (id, creator) ->
        view = @views[id]
        if not view
            view = @views[id] = creator()
            isNew = true
        else
            isNew = false

        if @current and @current != view
            $(@current.el).hide()
        @current = view

        if isNew
            @el.append view.render().el
        else
            $(view.el).show()

        return [view, isNew]

    # ## Views

    index: ->
        [form, isNew] = @getView('form', => new Form(app: this))

    set: (set, photo) ->
        [view, isNew] = @getView("set-#{set}", -> new SetView(id: set))
        if photo
            view.scrollTo(photo)

    user: (user) ->
        [view, isNew] = @getView("user-#{user}", -> new UserView(user: user))

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
