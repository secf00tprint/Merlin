// Later Webpack, Context etc. should be used instead.

global.testserver = 'http://localhost:8042';
global.restBaseUrl = (process.env.NODE_ENV === 'development' ? global.testserver : '') + '/rest';

export const getRestServiceUrl = restService => `${global.restBaseUrl}/${restService}`;

export const getResponseHeaderFilename = contentDisposition => {
    const matches = /filename[^;=\n]*=(UTF-8(['"]*))?(.*)/.exec(contentDisposition);
    return matches && matches.length >= 3 && matches[3] ? decodeURI(matches[3].replace(/['"]/g, '')) : 'download';
};

export const revisedRandId = () => Math.random().toString(36).replace(/[^a-z]+/g, '').substr(2, 10);
