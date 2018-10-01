import React from 'react'
import {PageHeader} from 'react-bootstrap';
import {getRestServiceUrl} from "../../../actions/global";
import ErrorAlert from "../../general/ErrorAlert";
import Template from "./Template";

class TemplateListView extends React.Component {


    path = getRestServiceUrl('templates');
    state = {
        isFetching: false
    };

    componentDidMount = () => {
        this.fetchTemplates();
    };

    fetchTemplates = () => {
        this.setState({
            isFetching: true,
            failed: false,
            definitions: undefined,
            templates: undefined
        });

        fetch(`${this.path}/list`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        })
            .then(response => response.json())
            .then(json => {
                const definitions = json.templateDefinitions.reduce((accumulator, current) => ({
                    ...accumulator,
                    [current.refId]: current
                }), {});

                const templates = json.templates.map(template => {
                    if (typeof template.templateDefinition === 'object') {
                        definitions[template.templateDefinition.refId] = template.templateDefinition;
                        template.templateDefinition = template.templateDefinition.refId;
                    }

                    return {
                        templateDefinition: template.templateDefinition,
                        fileDescriptor: template.fileDescriptor
                    };
                });

                this.setState({
                    isFetching: false,
                    definitions, templates
                })
            })
            .catch(() => this.setState({isFetching: false, failed: true}));
    };

    render = () => {
        let content = undefined;

        if (this.state.isFetching) {

            content = <i>Loading...</i>;

        } else if (this.state.failed) {

            content = <ErrorAlert
                title={'Cannot load Templates'}
                description={'Something went wrong during contacting the rest api.'}
                action={{
                    handleClick: this.fetchTemplates,
                    title: 'Try again'
                }}
            />;

        } else if (this.state.templates) {

            content = <div>
                {this.state.templates.map(template => {
                    const definition = this.state.definitions[template.templateDefinition];

                    return <Template
                        key={template.fileDescriptor.canonicalPath}
                        {...definition}
                    />;
                })}
            </div>;

        }

        return <div>
            <PageHeader>
                Templates
            </PageHeader>
            {content}
        </div>;
    };

    constructor(props) {
        super(props);

        this.fetchTemplates = this.fetchTemplates.bind(this);
    }
}

export default TemplateListView;
