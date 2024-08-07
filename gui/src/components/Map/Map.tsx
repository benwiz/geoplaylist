import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { styled } from '@stitches/react';

const ContainerDiv = styled('div', {
  height: '800px',
  padding: '20px',

  borderStyle: 'solid',
  borderColor: 'grey',
  borderWidth: '2px',
  borderRadius: '5px',

  '.leaflet-container': {
    height: '600px',
  },
});

const PopupContent = ({ place }) => {
  const { id, listens, trackIds } = place;
  return (
    // TODO require spotify to be logged in, include button to do so if not
    // TODO give the playlist a name via an input field
    // TODO generate playlist via a button click
    // TODO if playlist already exists (or was created via button click) show deep link to spotify
    // ... basically the demo component I created in App.tsx is very close to correct
    <span>
      Location {id}: {listens.toLocaleString()} listens across{' '}
      {trackIds.size.toLocaleString()} tracks
    </span>
  );
};

function Map({ places }) {
  return (
    <ContainerDiv>
      <MapContainer
        center={[37, -105]} // TODO intelligently pick starting point
        zoom={4}
        scrollWheelZoom={true}
        id={'map'}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
        />
        {Object.keys(places).map((id) => {
          const place = places[id];
          place.id = id;
          return (
            <Marker
              key={id}
              position={[place.lat, place.lng]}
            >
              <Popup>
                <PopupContent place={place} />
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>
    </ContainerDiv>
  );
}

export default Map;
