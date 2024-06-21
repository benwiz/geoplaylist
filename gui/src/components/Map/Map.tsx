import React, { useState, useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import './Map.css';

const PopupContent = ({ place }) => {
  const { id, listens, trackIds } = place;
  return (
    // TODO give the playlist a name
    // TODO generate playlist (require spotify to be logged in)
    // ... basically the demo component I created in App.tsx is very close to correct
    <span>
      Location {id}: {listens} listens across {trackIds.size} tracks
    </span>
  );
};

function Map({ places }) {
  return (
    <MapContainer
      center={[37,-105]} // TODO intelligently pick starting point
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
        return (
          <Marker key={id} position={[place.lat, place.lng]}>
            <Popup>
              <PopupContent place={place} />
            </Popup>
          </Marker>
        );
      })}
    </MapContainer>
  );
}

export default Map;
