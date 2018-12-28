import { Workspace } from '../models/Workspace';
import { Cluster } from '../models/Cluster';

export interface LoginState {
    token: boolean;
    error: boolean;
    loggingIn: boolean;
    loading: boolean;
    profile: boolean;
    profileLoading: boolean;
    workspace?: Workspace;
}

export interface ListingState {
    fetching: boolean;
    allWorkspaces: any;
    filters: {
      filter: string
      behaviors: string[],
      statuses: string[],
  };
}

export interface RiskState {
    fetching: boolean;
    workspaces: any;
}

export interface RequestState {
    generating: boolean;
    behavior: boolean;
    worksapce: boolean;
    request: boolean;
    requesting: boolean;
    template: boolean;
    page: number;
}

export interface ClusterState {
  details: Cluster;
}

export interface DetailsState {
    fetching: boolean;
    details: boolean;
}

export interface IState {
    login: LoginState;
    listing: ListingState;
    risk: RiskState;
    request: RequestState;
    cluster: ClusterState;
    details: DetailsState;
}
