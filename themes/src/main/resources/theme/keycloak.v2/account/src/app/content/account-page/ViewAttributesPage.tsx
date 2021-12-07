import * as React from 'react';

import {
    Checkbox,
    DataList,
    DataListItem,
    DataListItemRow,
    DataListCell,
    DataListItemCells,
} from '@patternfly/react-core';

import { ContentPage } from '../ContentPage';
import { HttpResponse } from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Msg } from '../../widgets/Msg';

export interface ViewAttributesProps {
}

export interface ViewAttributesState {
    attributes: Map<string, string[]>;
}


export class ViewAttributes extends React.Component<ViewAttributesProps, ViewAttributesState> {
    static contextType = AccountServiceContext;
    context: React.ContextType<typeof AccountServiceContext>;

    public constructor(props: ViewAttributesProps, context: React.ContextType<typeof AccountServiceContext>) {
        super(props);
        this.context = context;
        this.state = {
            attributes: new Map()
        };
        this.fetchUserAttributes();
    }

    private fetchUserAttributes(): void {
        this.context!.doGet<Map<string, string[]>>("/attributes")
            .then((response: HttpResponse<Map<string, string[]>>) => {
                const attributes = response.data || new Map();
                this.setState({
                    attributes: attributes
                });
            });
    }

    private renderAttributeValuesList(key:string, value: string, appIndex: number): React.ReactNode {

        return (
            <React.Fragment>
                <span> {value} </span><br />
            c</React.Fragment>
        )
    }

    public render(): React.ReactNode {
        return (
            <ContentPage title={Msg.localize('attributes')}>
                <DataList id="attributes-list" aria-label={Msg.localize('attributes')} isCompact>
                    <DataListItem id="attributes-list-header" aria-labelledby="Columns names">
                        <DataListItemRow >
                            <DataListItemCells
                                dataListCells={[
                                    <DataListCell key='key-header' width={2}>
                                        <strong><Msg msgKey='key' /></strong>
                                    </DataListCell>,
                                    <DataListCell key='value-header' width={2}>
                                        <strong><Msg msgKey='value' /></strong>
                                    </DataListCell>,
                                ]}
                            />
                        </DataListItemRow>
                    </DataListItem>
                    {Array.from(Object.keys(this.state.attributes)).map((key: string, index: number)  =>  {
                        return (
                            <DataListItem id={`${key}-group`} key={'group-' + key} aria-labelledby="attributes-list" >
                                <DataListItemRow>
                                    <DataListItemCells
                                        dataListCells={[
                                            <DataListCell id={`${key}-key`} width={2} key={'key-' + key}>
                                                {key}
                                            </DataListCell>,
                                            <DataListCell id={`${key}-value`} width={2} key={'value-' + key}>
                                                {this.state.attributes[key].map((value: string, appIndex: number) =>
                                                this.renderAttributeValuesList(key,value, appIndex))}
                                            </DataListCell>
                                        ]}
                                    />
                                </DataListItemRow>

                            </DataListItem>
                        );
                    }) }
                </DataList>
            </ContentPage>
        );
    }
};