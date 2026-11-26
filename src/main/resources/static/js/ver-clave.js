// a cada campo de contraseña le pone un ojito para ver lo que se escribio

document.querySelectorAll('input[type="password"]').forEach(function (campo) {
    const caja = document.createElement('span');
    caja.className = 'k-clave-caja';
    campo.parentNode.insertBefore(caja, campo);
    caja.appendChild(campo);

    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'k-ver-clave';
    boton.setAttribute('aria-label', 'Mostrar contraseña');
    boton.innerHTML = '<i class="bi bi-eye"></i>';
    caja.appendChild(boton);

    boton.addEventListener('click', function () {
        const oculta = campo.type === 'password';
        campo.type = oculta ? 'text' : 'password';
        boton.setAttribute('aria-label', oculta ? 'Ocultar contraseña' : 'Mostrar contraseña');
        boton.innerHTML = oculta ? '<i class="bi bi-eye-slash"></i>' : '<i class="bi bi-eye"></i>';
    });
});
