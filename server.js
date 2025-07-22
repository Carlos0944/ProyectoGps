const net = require('net');
const admin = require('firebase-admin');


const serviceAccount = JSON.parse(process.env.FIREBASE_KEY);



admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://gpsubicacionapp-1fea2-default-rtdb.firebaseio.com'
});

const db = admin.database();

const server = net.createServer(socket => {
  console.log('GPS conectado desde', socket.remoteAddress);

  socket.on('data', data => {
    const mensaje = data.toString();
    console.log('Datos recibidos:', mensaje);

    // Buscar mensaje con formato $GPRMC (puedes adaptar para otros)
    const match = mensaje.match(/\$GPRMC,[^*]+/);
    if (match) {
      const partes = match[0].split(',');

      const lat = convertirLatitud(partes[3], partes[4]);
      const lon = convertirLongitud(partes[5], partes[6]);

      const ref = db.ref('gps/vehiculo1'); // Puedes cambiar la ruta si quieres
      ref.set({
        latitud: lat,
        longitud: lon,
        timestamp: Date.now()
      });

      console.log(`Ubicación subida a Firebase: ${lat}, ${lon}`);
    }
  });

  socket.on('close', () => {
    console.log('GPS desconectado');
  });

  socket.on('error', err => {
    console.error('Error de conexión:', err.message);
  });
});

function convertirLatitud(coord, dir) {
  if (!coord || !dir) return 0;
  const grados = parseInt(coord.slice(0, 2));
  const minutos = parseFloat(coord.slice(2)) / 60;
  const decimal = grados + minutos;
  return dir === 'S' ? -decimal : decimal;
}

function convertirLongitud(coord, dir) {
  if (!coord || !dir) return 0;
  const grados = parseInt(coord.slice(0, 3));
  const minutos = parseFloat(coord.slice(3)) / 60;
  const decimal = grados + minutos;
  return dir === 'W' ? -decimal : decimal;
}

server.listen(5000, () => {
  console.log('Servidor TCP escuchando en puerto 5000');
});
