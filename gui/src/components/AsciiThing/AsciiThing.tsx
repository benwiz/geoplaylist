export const fetchAsciiArt = async () => {
  try {
    return (await fetch('./jurassic_park.txt')).text();
  } catch (error) {
    console.error('Error fetching ASCII art:', error);
  }
};
