export const REQUEST_SHARED_WORKSPACE = "REQUEST_SHARED_WORKSPACE";


export function requestSharedWorkspace({name, purpose, phi_data, pii_data, pci_data}) {
    return {
        type: REQUEST_SHARED_WORKSPACE,
        request: {
            name,
            purpose,
            compliance: {
                pii_data: !!pii_data,
                pci_data: !!pci_data,
                phi_data: !!phi_data
            },
            requested_size_in_gb: 12,
            requested_cores: 10,
            requested_memory_in_gb: 8
        }
    }
}