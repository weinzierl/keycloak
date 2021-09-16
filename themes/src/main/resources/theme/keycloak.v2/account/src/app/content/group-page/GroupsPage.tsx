import * as React from 'react';

import {
  Checkbox,
  DataList,
  DataListItem,
  DataListItemRow,
  DataListCell,
  DataListItemCells,
  Button,
  Modal,
  TextInput,
  Level,
  LevelItem,
  Stack,
  StackItem
} from '@patternfly/react-core';
import { ContentPage } from '../ContentPage';
import { ContentAlert } from '../ContentAlert';
import { HttpResponse } from '../../account-service/account.service';
import { AccountServiceContext } from '../../account-service/AccountServiceContext';
import { Msg } from '../../widgets/Msg';
import parse from '../../util/ParseLink';
import { Features } from '../../widgets/features';

declare const features: Features;

export interface GroupsPageProps {
}

export interface GroupsPageState {
  groups: Group[];
  directGroups: Group[];
  allGroups: PaginatedGroups;
  //all group membership (including inheritance) paths
  groupsPaths: string[];
  isDirectMembership: boolean;
  isOpened: boolean;
  joinGroups: string[];
  search: string;
  groupInRequests: string[];
}

interface PaginatedGroups {
  nextUrl: string;
  prevUrl: string;
  data: Group[];
}

interface Group {
  id?: string;
  name: string;
  path: string;
  subGroups?: Group[];
}

interface GroupJoinRequest {
  joinGroups: string[];
  reason?: string;
}

export class GroupsPage extends React.Component<GroupsPageProps, GroupsPageState> {
  static contextType = AccountServiceContext;
  context: React.ContextType<typeof AccountServiceContext>;
  private isJoinGroupsEnabled: boolean = features.isJoinGroupsEnabled;

  public constructor(props: GroupsPageProps, context: React.ContextType<typeof AccountServiceContext>) {
    super(props);
    this.context = context;
    this.state = {
      groups: [],
      directGroups: [],
      allGroups: { nextUrl: '', prevUrl: '', data: [] },
      groupsPaths: [],
      isDirectMembership: false,
      isOpened: false,
      joinGroups: [],
      search: '',
      groupInRequests: []
    };
    this.fetchGroups();
  }

  private fetchGroups(): void {
      this.context!.doGet<Group[]>("/groups")
          .then((response: HttpResponse<Group[]>) => {
              const directGroups = response.data || [];
              const groups = [...directGroups];
              const groupsPaths = directGroups.map(s => s.path);
              directGroups.forEach((el) => this.getParents(el, groups, groupsPaths))
              this.setState({
                  groups: groups,
                  directGroups: directGroups,
                  groupsPaths: groupsPaths
              });
              //this.fetchAllGroups("/groups/all");
      });
      //get groupsId of pending requests - disable these groups
      this.context!.doGet<string[]>("/groups/requests")
          .then((response: HttpResponse<string[]>) => {
              this.setState({
                  groupInRequests: response.data || []
              });
      });
  }

  private getParents(el: Group, groups: Group[], groupsPaths: string[]): void {
    const parentPath = el.path.slice(0, el.path.lastIndexOf('/'));
    if (parentPath && (groupsPaths.indexOf(parentPath) === -1)) {
      el = {
        name: parentPath.slice(parentPath.lastIndexOf('/') + 1),
        path: parentPath
      };
      groups.push(el);
      groupsPaths.push(parentPath);
      this.getParents(el, groups, groupsPaths);
    }
  }

  private changeDirectMembership = (checked: boolean, event: React.FormEvent<HTMLInputElement>) => {
    this.setState({
      isDirectMembership: checked
    });
  }

  private handleToggleDialog = () => {
      if (!this.state.isOpened)
           this.fetchAllGroups("/groups/all");
      this.setState({
          isOpened: !this.state.isOpened, joinGroups: [], search: ''
      });
  }

  private requestJoinFunction = () => {
      const reasonValue = (document.getElementById('reason') as HTMLInputElement).value;
      const request : GroupJoinRequest = {
          joinGroups: this.state.joinGroups,
          reason: reasonValue
      };
      this.context!.doPost<void>("/groups/join", request)
          .then(() => {
              this.setState({
                  isOpened: !this.state.isOpened, joinGroups: []
              });
              ContentAlert.success('successfulRequest');
      });
  }

  private checkboxJoinGroup = (id: string, checked: boolean) => {
      if (checked) {
          this.state.joinGroups.push(id);
          this.setState({
              joinGroups: this.state.joinGroups
          });
      } else {
          this.setState({
             joinGroups: this.state.joinGroups.filter(item => item !== id)
         });
      }
  }

  private fetchAllGroups = async (url: string, extraParams?: Record<string, string | number>) => {
      const response: HttpResponse<Group[]> = await this.context!.doGet(url, { params: extraParams });
      this.setState({
          allGroups: this.parseGroupsResponse(response)
      });
  }

  private parseGroupsResponse(response: HttpResponse<Group[]>): PaginatedGroups {
      const links: string | undefined = response.headers.get('link') || undefined;
      const parsed = parse(links);

      let next = '';
      let prev = '';

      if (parsed !== null) {
          if (parsed.next) next = parsed.next;
          if (parsed.prev) prev = parsed.prev;
      }

      const groupLists: Group[] = response.data || [];
      return { nextUrl: next, prevUrl: prev, data: groupLists };
  }

  private hasNext(): boolean {
      return (this.state.allGroups.nextUrl !== null) && (this.state.allGroups.nextUrl !== '');
  }

  private hasPrevious(): boolean {
      return (this.state.allGroups.prevUrl !== null) && (this.state.allGroups.prevUrl !== '');
  }

  private handleFilterRequest = (value: string) => {
      this.setState({ search: value });
      this.fetchAllGroups("/groups/all", { search: value });
  }

  private handleFirstPageClick = () => {
      this.fetchAllGroups("/groups/all", { search: this.state.search });
  }

  private handleNextClick = () => {
      this.fetchAllGroups(this.state.allGroups.nextUrl);
  }

  private handlePreviousClick = () => {
      this.fetchAllGroups(this.state.allGroups.prevUrl);
  }

  private modal(): React.ReactNode {
      return (
          <Modal
              id={'modal-join-group'}
              title={Msg.localize('joinGroupRequest')}
              isLarge={true}
              isOpen={this.state.isOpened}
              onClose={this.handleToggleDialog}
              height='2xl'
          >

              <Stack gutter="md">
                  <StackItem isFilled>
                      <Level gutter='md'>
                          <LevelItem>
                              <TextInput value={this.state.search} onChange={this.handleFilterRequest} id="filter-groups" type="text" placeholder={Msg.localize('filterByName')} />
                          </LevelItem>
                      </Level>
                  </StackItem>
                  <StackItem isFilled>
                      <DataList id="modal-groups-list" aria-label={Msg.localize('joinGroupRequest')} isCompact>
                          <DataListItem id="modal-groups-list-header" aria-labelledby="Columns names">
                              <DataListItemRow >
                                  <DataListItemCells dataListCells={[
                                      <DataListCell key='group-name-header' width={2}>
                                          <strong><Msg msgKey='Name' /></strong>
                                      </DataListCell>,
                                      <DataListCell key='group-path-header' width={2}>
                                          <strong><Msg msgKey='path' /></strong>
                                      </DataListCell>
                                  ]} />
                              </DataListItemRow>
                          </DataListItem>
                          {this.state.allGroups.data.length === 0
                              ? this.emptyGroup('noGroups')
                              : this.state.allGroups.data.map((group: Group, appIndex: number) =>
                              this.renderJoinGroupList(group, appIndex))}
                      </DataList>
                  </StackItem>
              </Stack>
              <Level gutter='md'>
                  <LevelItem >
                      {this.hasPrevious() && <Button sizes='sm' onClick={this.handlePreviousClick}>&lt;<Msg msgKey='previousPage' /></Button>}
                  </LevelItem>

                  <LevelItem >
                      {this.hasPrevious() && <Button sizes='sm' onClick={this.handleFirstPageClick}><Msg msgKey='firstPage' /></Button>}
                  </LevelItem>

                  <LevelItem >
                      {this.hasNext() && <Button sizes='sm' onClick={this.handleNextClick}><Msg msgKey='nextPage' />&gt;</Button>}
                  </LevelItem>
              </Level>
              <TextInput
                  title={Msg.localize('reasonMessage')}
                  placeholder={Msg.localize('reasonMessage')}
                  type="text"
                  id="reason"
                  name="reason"
                  maxLength={254}
              />
              <Button key="joingroup" variant="primary" id="joingroup-button" isDisabled={this.state.joinGroups.length == 0} onClick={this.requestJoinFunction}>
                  <Msg msgKey='joingroup' />
              </Button>
          </Modal>
      )
  }

  private renderJoinGroupList(group: Group, appIndex: number): React.ReactNode {
      return (
          <React.Fragment>
              <DataListItem key={'group-all-' + appIndex} aria-labelledby="groups-all" >
                  <DataListItemRow>
                      <DataListItemCells dataListCells={[
                          <DataListCell width={2} key={'name-' + appIndex}>
                              <Checkbox
                                  label={group.name}
                                  id="join-groupname-checkbox-${appIndex}"
                                  isDisabled={this.state.groupsPaths.indexOf(group.path) > -1 || this.state.groupInRequests.indexOf(group.id ?? '') > -1}
                                  isChecked={this.state.groupsPaths.indexOf(group.path) > -1 || this.state.joinGroups.indexOf(group.id ?? '') > -1}
                                  onChange={(checked: boolean, e: React.FormEvent<HTMLInputElement>) => this.checkboxJoinGroup(group.id ?? '', checked)}
                             />
                          </DataListCell>,
                          <DataListCell width={2} key={'path-' + appIndex}>
                              {group.path}
                          </DataListCell>
                      ]} />
                  </DataListItemRow>
              </DataListItem>
          </React.Fragment>
     )
  }

  private emptyGroup(text: string): React.ReactNode {

    return (
      <DataListItem key='emptyItem' aria-labelledby="empty-item">
        <DataListItemRow key='emptyRow'>
          <DataListItemCells dataListCells={[
            <DataListCell key='empty'><strong><Msg msgKey={text} /></strong></DataListCell>
          ]} />
        </DataListItemRow>
      </DataListItem>
    )
  }

  private renderGroupList(group: Group, appIndex: number): React.ReactNode {

    return (
      <DataListItem id={`${appIndex}-group`} key={'group-' + appIndex} aria-labelledby="groups-list" >
        <DataListItemRow>
          <DataListItemCells
            dataListCells={[
              <DataListCell id={`${appIndex}-group-name`} width={2} key={'name-' + appIndex}>
                {group.name}
              </DataListCell>,
              <DataListCell id={`${appIndex}-group-path`} width={2} key={'path-' + appIndex}>
                {group.path}
              </DataListCell>,
              <DataListCell id={`${appIndex}-group-directMembership`} width={2} key={'directMembership-' + appIndex}>
                <Checkbox id={`${appIndex}-checkbox-directMembership`} isChecked={group.id != null} isDisabled={true} />
              </DataListCell>
            ]}
          />
        </DataListItemRow>

      </DataListItem>
    )
  }

    private renderJoinButton(): React.ReactNode {
        return (
            <DataListCell key='join-group-header' width={5}>
                <Button key="joingroup" variant="primary" id="joingroup" onClick={this.handleToggleDialog}>
                    <Msg msgKey='joingroup' />
                </Button>
            </DataListCell>
        )
    }

  public render(): React.ReactNode {
    return (
      <ContentPage title={Msg.localize('groupLabel')}>
        <DataList id="groups-list" aria-label={Msg.localize('groupLabel')} isCompact>
          <DataListItem id="groups-list-header" aria-labelledby="Columns names" >
            <DataListItemRow >
              {this.isJoinGroupsEnabled ? <DataListItemCells
                dataListCells={[
                  <DataListCell key='directMembership-header' width={1}>
                    <Checkbox
                      label={Msg.localize('directMembership')}
                      id="directMembership-checkbox"
                      isChecked={this.state.isDirectMembership}
                      onChange={this.changeDirectMembership}
                    />
                  </DataListCell>,
                  <DataListCell key='join-group-header' width={5}>
                                          <Button key="joingroup" variant="primary" id="joingroup" onClick={this.handleToggleDialog}>
                                            <Msg msgKey='joingroup' />
                                          </Button>
                  </DataListCell>
                ]}
              />

               : <DataListItemCells
                   dataListCells={[
                     <DataListCell key='directMembership-header' width={1}>
                       <Checkbox
                         label={Msg.localize('directMembership')}
                         id="directMembership-checkbox"
                         isChecked={this.state.isDirectMembership}
                         onChange={this.changeDirectMembership}
                       />
                     </DataListCell>
                 ]}
               /> }
                {this.modal()}
            </DataListItemRow>
          </DataListItem>
          <DataListItem id="groups-list-header" aria-labelledby="Columns names">
            <DataListItemRow >
              <DataListItemCells
                dataListCells={[
                  <DataListCell key='group-name-header' width={2}>
                    <strong><Msg msgKey='Name' /></strong>
                  </DataListCell>,
                  <DataListCell key='group-path-header' width={2}>
                    <strong><Msg msgKey='path' /></strong>
                  </DataListCell>,
                  <DataListCell key='group-direct-membership-header' width={2}>
                    <strong><Msg msgKey='directMembership' /></strong>
                  </DataListCell>,
                ]}
              />
            </DataListItemRow>
          </DataListItem>
          {this.state.groups.length === 0
            ? this.emptyGroup('noGroupsText')
            : (this.state.isDirectMembership ? this.state.directGroups.map((group: Group, appIndex: number) =>
              this.renderGroupList(group, appIndex)
            ) : this.state.groups.map((group: Group, appIndex: number) =>
              this.renderGroupList(group, appIndex)))}
        </DataList>
      </ContentPage>
    );
  }
};
