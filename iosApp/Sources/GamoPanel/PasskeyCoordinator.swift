import Foundation
import Combine
import AuthenticationServices
import UIKit

/// Puente entre Compose Multiplatform y las passkeys nativas de iOS/iPadOS.
/// El JSON resultante conserva el formato PublicKeyCredential esperado por SimpleWebAuthn.
final class PasskeyCoordinator: NSObject, ObservableObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var requestObserver: NSObjectProtocol?
    private var activeController: ASAuthorizationController?

    override init() {
        super.init()
        requestObserver = NotificationCenter.default.addObserver(
            forName: Notification.Name("GamoPasskeyRequest"), object: nil, queue: .main
        ) { [weak self] note in
            guard let self,
                  let options = note.userInfo?["optionsJson"] as? String,
                  let rpId = note.userInfo?["rpId"] as? String else { return }
            self.authenticate(optionsJSON: options, relyingPartyId: rpId)
        }
    }

    deinit { if let requestObserver { NotificationCenter.default.removeObserver(requestObserver) } }

    private func authenticate(optionsJSON: String, relyingPartyId: String) {
        do {
            guard let root = try JSONSerialization.jsonObject(with: Data(optionsJSON.utf8)) as? [String: Any],
                  let challengeText = root["challenge"] as? String,
                  let challenge = Self.decodeBase64URL(challengeText) else {
                throw NSError(domain: "GamoPanel.Passkey", code: 1, userInfo: [NSLocalizedDescriptionKey: "Challenge WebAuthn inválido"])
            }
            let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier: relyingPartyId)
            let request = provider.createCredentialAssertionRequest(challenge: challenge)
            if let allowed = root["allowCredentials"] as? [[String: Any]] {
                request.allowedCredentials = allowed.compactMap { item in
                    guard let id = item["id"] as? String, let data = Self.decodeBase64URL(id) else { return nil }
                    return ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID: data)
                }
            }
            request.userVerificationPreference = .required
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            activeController = controller
            controller.performRequests()
        } catch { publishFailure(error.localizedDescription) }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first(where: \.isKeyWindow) ?? UIWindow()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let assertion = authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialAssertion else {
            publishFailure("Apple no devolvió una assertion de passkey compatible."); return
        }
        let response: [String: Any] = [
            "id": Self.encodeBase64URL(assertion.credentialID),
            "rawId": Self.encodeBase64URL(assertion.credentialID),
            "type": "public-key",
            "authenticatorAttachment": "platform",
            "response": [
                "clientDataJSON": Self.encodeBase64URL(assertion.rawClientDataJSON),
                "authenticatorData": Self.encodeBase64URL(assertion.rawAuthenticatorData),
                "signature": Self.encodeBase64URL(assertion.signature),
                "userHandle": assertion.userID.map(Self.encodeBase64URL) as Any? ?? NSNull()
            ],
            "clientExtensionResults": [String: Any]()
        ]
        do {
            let data = try JSONSerialization.data(withJSONObject: response, options: [.fragmentsAllowed, .sortedKeys])
            NotificationCenter.default.post(name: Notification.Name("GamoPasskeyResult"), object: nil, userInfo: ["assertionJson": String(decoding: data, as: UTF8.self)])
        } catch { publishFailure(error.localizedDescription) }
        activeController = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        publishFailure(error.localizedDescription)
        activeController = nil
    }

    private func publishFailure(_ message: String) {
        NotificationCenter.default.post(name: Notification.Name("GamoPasskeyResult"), object: nil, userInfo: ["error": message])
    }
    private static func decodeBase64URL(_ value: String) -> Data? {
        var text = value.replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
        text += String(repeating: "=", count: (4 - text.count % 4) % 4)
        return Data(base64Encoded: text)
    }
    private static func encodeBase64URL(_ data: Data) -> String {
        data.base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
    }
}
