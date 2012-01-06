(->
    id = ("showkr" + Math.random()).replace('.', '');
    document.write("<link rel='stylesheet' href='http://showkr.solovyov.net/namespaced.css'>
    <script src='http://showkr.solovyov.net/app.js'></script>
    <div class='showkr'><div class='container'><div class='row'><div id='#{id}' class='span16'></div></div></div></div>
    <script>
    window._showkr = new Showkr('##{id}', $('script[data-set]').attr('data-set'));
    </script>
    ");
)();
