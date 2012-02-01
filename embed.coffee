(->
    id = ("showkr" + Math.random()).replace('.', '');
    document.write("<link rel='stylesheet' href='http://showkr.org/namespaced.css'>
    <script src='http://showkr.org/app.js'></script>
    <div class='showkr'><div class='container'><div class='row'><div id='#{id}' class='span12'></div></div></div></div>
    <script>
    window.ender.noConflict();
    window._showkr = new Showkr('##{id}', ender('script[src=\"http://showkr.org/embed.js\"]').data());
    </script>
    ");
)();
