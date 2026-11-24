// Linhas opcionais só exigem identificação quando algum dado do material foi informado.
document.querySelectorAll('.itens-form').forEach((linha) => {
  const campos = [...linha.querySelectorAll('input, select')];
  const identificacao = linha.querySelector('input[name$=".identificacao"]');
  if (!identificacao) return;
  const validar = () => {
    const preenchida = campos.some((campo) => campo.value.trim() !== '');
    identificacao.required = preenchida;
    identificacao.setAttribute('aria-required', String(preenchida));
    identificacao.setCustomValidity(
      preenchida && identificacao.value.trim() === '' ? 'Identifique este equipamento.' : ''
    );
  };
  campos.forEach((campo) => {
    campo.addEventListener('input', validar);
    campo.addEventListener('change', validar);
  });
  validar();
});
