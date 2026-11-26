// O formulário Java interpreta datetime-local em America/Sao_Paulo.
// Intl usa o mesmo identificador, sem depender do fuso local do computador.
const zone = 'America/Sao_Paulo';
const formatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
});
function parts(date) {
  return Object.fromEntries(formatter.formatToParts(date).map(p => [p.type, p.value]));
}
function stamp(date) {
  const p = parts(date);
  return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`;
}
function localToInstant(value) {
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value))
    throw new Error('PI_AGENDAMENTO deve usar YYYY-MM-DDTHH:mm no fuso America/Sao_Paulo.');
  const [year, month, day, hour, minute] = value.match(/\d+/g).map(Number);
  const wall = Date.UTC(year, month - 1, day, hour, minute);
  let candidate = wall;
  // Corrige o instante pela representação do horário civil no fuso desejado.
  for (let i = 0; i < 3; i++) {
    const p = parts(new Date(candidate));
    const represented = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute);
    candidate += wall - represented;
  }
  if (stamp(new Date(candidate)) !== value)
    throw new Error('PI_AGENDAMENTO contém data/hora inválida ou inexistente em São Paulo.');
  return new Date(candidate);
}
export function scheduleForDemo(override, now = new Date()) {
  const value = override || stamp(new Date(now.getTime() + 48 * 60 * 60 * 1000));
  const instant = localToInstant(value);
  if (instant <= now) throw new Error('PI_AGENDAMENTO precisa estar no futuro em America/Sao_Paulo.');
  return { value, timeZone: zone, instant: instant.toISOString() };
}
export function validateDemoUrls(a, b) {
  if (![a, b].every(u => /^http:\/\/127\.0\.0\.1:\d+$/.test(u)))
    throw new Error('Ensaio automático restrito a aplicações locais de demonstração.');
  for (const value of [a, b]) {
    const url = new URL(value);
    if (!url.port || +url.port < 1 || +url.port > 65535) throw new Error('Porta inválida.');
  }
  if (new URL(a).href === new URL(b).href)
    throw new Error('PI_APP_A e PI_APP_B devem indicar instâncias distintas.');
}
