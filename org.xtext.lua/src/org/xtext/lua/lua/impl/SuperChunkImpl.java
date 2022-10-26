package org.xtext.lua.lua.impl;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.SuperChunk;

public class SuperChunkImpl extends MinimalEObjectImpl.Container implements SuperChunk {
    private EList<Chunk> chunks;

    @Override
    public EList<Chunk> getChunks() {
        if (chunks == null) {
            // this 0 is probably not correct, as we have no feature id for our custom class
//            chunks = new EObjectContainmentEList<Chunk>(Chunk.class, this, 0);
            chunks = new BasicEList<Chunk>();
        }
        return chunks;
    }

}
