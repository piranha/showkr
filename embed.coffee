(->
    id = ("showkr" + Math.random()).replace('.', '');
    document.write("<link rel='stylesheet' href='http://localhost/showkr-prod/namespaced.css'>
    <script src='http://localhost/showkr-prod/app.js'></script>
    <div class='showkr'><div class='container'><div class='row'><div id='#{id}' class='span16'></div></div></div></div>
    <script>
    window.ender.noConflict();
    window._showkr = new Showkr('##{id}', ender('script[src=\"http://localhost/showkr-prod/embed.js\"]').data());
    </script>
    ");
)();
