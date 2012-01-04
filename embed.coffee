(->
    id = ("showkr" + Math.random()).replace('.', '');
    document.write("<link rel='stylesheet' href='namespaced.css'>
    <script src='app.js'></script>
    <div class='showkr'><div class='container'><div class='row'><div id='#{id}' class='span16'></div></div></div></div>
    <script>
    window.app = new Showkr('##{id}', $('script[data-set]').attr('data-set'));
    </script>
    ");
)();
