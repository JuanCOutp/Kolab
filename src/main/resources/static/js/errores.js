// si algo de javascript falla en el navegador, se avisa al servidor para que quede en el log

(function () {
    const token = document.querySelector('meta[name="_csrf"]');
    const cabecera = document.querySelector('meta[name="_csrf_header"]');
    // una pagina que falla en bucle no tiene que llenar el log
    let enviados = 0;

    function avisar(mensaje, archivo, linea) {
        if (enviados >= 5) {
            return;
        }
        enviados++;
        const cabeceras = {'Content-Type': 'application/json'};
        if (token && cabecera) {
            cabeceras[cabecera.content] = token.content;
        }
        fetch('/errores-cliente', {
            method: 'POST',
            headers: cabeceras,
            body: JSON.stringify({mensaje: mensaje, archivo: archivo, linea: linea, pagina: location.pathname})
        }).catch(function () {
            // si ni esto llega, no hay mas que hacer
        });
    }

    // un error de un script de la pagina
    window.addEventListener('error', function (evento) {
        avisar(evento.message, evento.filename, evento.lineno);
    });

    // una promesa que fallo y nadie atrapo, por ejemplo un fetch
    window.addEventListener('unhandledrejection', function (evento) {
        avisar(String(evento.reason), '', 0);
    });
})();
