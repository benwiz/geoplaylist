const fetchAsciiArt = async (): Promise<string> => {
  try {
    const response = await fetch('./jurassic_park.txt');
    const data = await response.text();
    if (data !== undefined) {
      return data;
    } else {
      throw new Error('Data is undefined');
    }
  } catch (error) {
    console.error('Error fetching data:', error);
    throw error;
  }
};

export default fetchAsciiArt;
