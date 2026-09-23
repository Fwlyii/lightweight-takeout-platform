package elm_bk.service;

import elm_bk.entity.Person;
import elm_bk.mapper.PersonMapper;
import elm_bk.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/** Upload bytes for the authenticated account, never accept another user's ID or an arbitrary URL. */
@Service
@RequiredArgsConstructor
public class AvatarUpdateService {
    private final AccountWriteLock writeLock;
    private final PersonMapper people;
    private final ImageStorageService images;
    private final CurrentProfileService profile;

    @Transactional(rollbackFor = Exception.class)
    public PersonVO update(MultipartFile avatar) throws IOException {
        Long id = writeLock.acquire();
        String stored = images.storeImage(avatar);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) images.discard(stored);
            }
        });
        Person person = new Person();
        person.setId(id);
        person.setPhoto(stored);
        people.updateById(person);
        // Keep the old file: legacy records may share a URL with another resource.
        return profile.get();
    }
}
