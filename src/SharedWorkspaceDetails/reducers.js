import {SHARED_WORKSPACE_DETAILS, WORKSPACE_MEMBER_LIST} from "./actions";

const initialState = {
    workspace: false,
    members: false
};

function sharedWorkspaceDetails(state = initialState, action) {
    switch(action.type) {
        case SHARED_WORKSPACE_DETAILS:
            return {
                ...state,
                workspace: action.workspace
            };
        case WORKSPACE_MEMBER_LIST:
            return {
                ...state,
                members: action.members
            };


        default:
            return state;
    }
}

export default sharedWorkspaceDetails;